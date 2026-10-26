package edu.kle.permits;

import java.time.Clock;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.web.cors.*;
import java.util.List;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean Clock clock(){return Clock.systemUTC();}
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecretKeySpec jwtKey(@Value("${app.jwt-secret}")String secret){
  if(secret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("JWT_SECRET must have at least 32 bytes.");
  return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
 }
 @Bean JwtEncoder jwtEncoder(SecretKeySpec key){return new NimbusJwtEncoder(new ImmutableSecret<>(key));}
 @Bean JwtDecoder jwtDecoder(SecretKeySpec key){var d=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();d.setJwtValidator(JwtValidators.createDefaultWithIssuer("permit-engine1"));return d;}
 @Bean FilterRegistrationBean<SessionFilter> noDoubleFilter(SessionFilter f){var bean=new FilterRegistrationBean<>(f);bean.setEnabled(false);return bean;}
 @Bean CorsConfigurationSource cors(@Value("${app.allowed-origin}")String origin){var c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PATCH","OPTIONS"));c.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN"));c.setAllowCredentials(true);var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",c);return source;}
 @Bean SecurityFilterChain security(HttpSecurity http,SessionFilter filter)throws Exception{
  var csrf=new CookieCsrfTokenRepository();csrf.setCookieHttpOnly(false);csrf.setCookiePath("/");
  var handler=new CsrfTokenRequestAttributeHandler();handler.setCsrfRequestAttributeName(null);
  return http.cors(c->{}).csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(handler))
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .formLogin(f->f.disable()).httpBasic(b->b.disable())
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/login","/api/support","/api/support/status").permitAll()
    .requestMatchers("/api/admin/**","/swagger-ui/**","/v3/api-docs/**").hasRole("ADMIN")
    .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
   .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->{r.setStatus(401);r.setContentType("application/json");r.getWriter().write("{\"message\":\"Please log in. Your session may have expired.\"}");})
    .accessDeniedHandler((q,r,x)->{r.setStatus(403);r.setContentType("application/json");r.getWriter().write("{\"message\":\"Access denied or security token invalid. Refresh and try again.\"}");}))
   .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
 }
}
