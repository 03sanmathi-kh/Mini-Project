package edu.kle.permits;
import java.nio.file.*;
import java.io.IOException;
import java.security.*;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Persistent content-addressed files; user-supplied filenames never select filesystem paths. */
@Component public class DocumentStore {
 private final Path root;
 public DocumentStore(@Value("${app.upload-dir:./uploads}")String directory)throws IOException{root=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(root);}
 public String path(Long application,String sha){if(application==null||application<1||!sha.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid document storage key");return application+"/"+sha;}
 public void put(Long application,String sha,byte[] bytes)throws IOException{
  Path target=resolve(path(application,sha));Files.createDirectories(target.getParent());
  if(Files.exists(target)){verify(sha,Files.readAllBytes(target));return;}
  Path temp=Files.createTempFile(target.getParent(),"upload-",".tmp");try{Files.write(temp,bytes);verify(sha,bytes);try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
 }
 public byte[] read(Long application,String sha)throws IOException{byte[] bytes=Files.readAllBytes(resolve(path(application,sha)));verify(sha,bytes);return bytes;}
 public boolean exists(Long application,String sha){return Files.isRegularFile(resolve(path(application,sha)),LinkOption.NOFOLLOW_LINKS);}
 private Path resolve(String relative){Path p=root.resolve(relative).normalize();if(!p.startsWith(root)||Files.isSymbolicLink(p)||Files.isSymbolicLink(p.getParent()))throw new IllegalArgumentException("Invalid document storage path");return p;}
 private void verify(String sha,byte[] bytes)throws IOException{try{if(!sha.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))))throw new IOException("Document integrity check failed");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
