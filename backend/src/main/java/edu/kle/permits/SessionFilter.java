package edu.kle.permits;

import java.io.IOException;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
@Component
public class SessionFilter extends OncePerRequestFilter {
 final JwtDecoder decoder;final AuthService auth;
 public SessionFilter(JwtDecoder decoder,AuthService auth){this.decoder=decoder;this.auth=auth;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String path=req.getRequestURI().substring(req.getContextPath().length());
  if(path.equals("/api/auth/login")||path.equals("/api/support")||path.equals("/api/support/status")||!(path.startsWith("/api/")||path.startsWith("/swagger-ui/")||path.startsWith("/v3/api-docs"))||req.getMethod().equals("OPTIONS")){chain.doFilter(req,res);return;}
  String token=null;
  if(req.getCookies()!=null)for(Cookie c:req.getCookies())if(c.getName().equals("PERMIT_TOKEN"))token=c.getValue();
  if(token!=null)try{
   // Read-only checks / polling never extend idle sessions. Only explicit activity or mutations do.
   boolean activity=path.equals("/api/auth/activity")||(!req.getMethod().equals("GET")&&!req.getMethod().equals("HEAD"));
   AuthService.Verified v=auth.verify(decoder.decode(token),activity);
   if(v!=null){
    var authorities=new ArrayList<SimpleGrantedAuthority>();authorities.add(new SimpleGrantedAuthority("ROLE_"+v.user().role.name()));
    v.user().role.permissions.forEach(p->authorities.add(new SimpleGrantedAuthority(p)));
    var authentication=new UsernamePasswordAuthenticationToken(v.user().id.toString(),null,authorities);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    req.setAttribute("currentUser",v.user());req.setAttribute("sessionId",v.session().id);
   }
  }catch(JwtException|IllegalArgumentException ignored){}
  chain.doFilter(req,res);
 }
}
