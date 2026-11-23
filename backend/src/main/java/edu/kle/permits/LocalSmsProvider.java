package edu.kle.permits;
import org.springframework.stereotype.Component;
@Component class LocalSmsProvider implements SmsProvider {
 public String send(Long recipientId,String event){return "MOCK_NOT_SENT";}
}
