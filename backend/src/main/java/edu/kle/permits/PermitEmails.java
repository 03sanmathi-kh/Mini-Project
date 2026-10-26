package edu.kle.permits;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public final class PermitEmails {
 private static final Pattern LOCAL=Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");
 private static final Pattern FULL=Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}@permitworks\\.com$");
 private static final Pattern PERSONAL=Pattern.compile("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$");
 private PermitEmails(){}
 public static String fromLocalPart(String value){String local=value==null?"":value.trim();if(local.isEmpty())throw bad("Email local part is required.");if(local.contains("@"))throw bad("Enter only the part before @permitworks.com.");if(!LOCAL.matcher(local).matches())throw bad("Use only letters, numbers, dots, underscores or hyphens in the email local part.");return local.toLowerCase(Locale.ROOT)+"@permitworks.com";}
 public static String normalizeFull(String value){String email=value==null?"":value.trim().toLowerCase(Locale.ROOT);if(!FULL.matcher(email).matches())throw bad("Enter a valid @permitworks.com email address.");return email;}
 public static String normalizePersonal(String value){String email=value==null?"":value.trim().toLowerCase(Locale.ROOT);if(email.length()>254||!PERSONAL.matcher(email).matches())throw bad("Enter a valid personal contact email address.");return email;}
 private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
