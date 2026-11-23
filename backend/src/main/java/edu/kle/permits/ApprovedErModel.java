package edu.kle.permits;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity @Table(name="applicants") class ApplicantProfile {
 @Id @Column(name="applicant_id")Long id;String name;String email;String passwordHash;String address;String phoneNo;
}
@Entity @Table(name="officers") class OfficerProfile {
 @Id @Column(name="officer_id")Long id;String name;String email;String passwordHash;@Column(name="phone_number")String phoneNo;@Column(name="official_role")String officerRole;
}
@Entity @Table(name="properties") class PropertyRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="property_id")Long id;
 Long ownerId;String address;String streetNumber;String streetDirection;String streetName;String zipCode;String ward;@Column(name="community_area")String communityType;
 @Column(precision=10,scale=7)BigDecimal latitude;
 @Column(precision=10,scale=7)BigDecimal longitude;
 protected PropertyRecord(){}
 PropertyRecord(String address,ApprovedErModel.PropertyInput d){this.address=address;if(d!=null){streetNumber=clean(d.streetNumber());streetDirection=clean(d.streetDirection());streetName=clean(d.streetName());zipCode=clean(d.zipCode());ward=clean(d.ward());communityType=clean(d.communityArea());latitude=d.latitude();longitude=d.longitude();}}
 private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
interface Properties extends JpaRepository<PropertyRecord,Long>{List<PropertyRecord> findByOwnerIdOrderById(Long owner);}

public final class ApprovedErModel {
 private ApprovedErModel(){}
 public record PropertyInput(@Size(max=30)String streetNumber,@Size(max=20)String streetDirection,@Size(max=150)String streetName,@Size(max=20)String zipCode,@Size(max=40)String ward,@Size(max=100)String communityArea,@DecimalMin("-90")@DecimalMax("90")@Digits(integer=3,fraction=7)BigDecimal latitude,@DecimalMin("-180")@DecimalMax("180")@Digits(integer=3,fraction=7)BigDecimal longitude){}
 public record PropertyView(Long id,String address,String streetNumber,String streetDirection,String streetName,String zipCode,String ward,String communityArea,BigDecimal latitude,BigDecimal longitude){static PropertyView of(PropertyRecord p){return new PropertyView(p.id,p.address,p.streetNumber,p.streetDirection,p.streetName,p.zipCode,p.ward,p.communityType,p.latitude,p.longitude);}}
 public record ProfileInput(@Size(max=300)String address,@Size(max=30)@Pattern(regexp="^$|^\\+?[0-9][0-9 ()-]{6,29}$")String phoneNo){}
 public record PropertyCreate(@NotBlank @Size(max=300)String address,@jakarta.validation.Valid @NotNull PropertyInput details){}
 public record ProfileView(Long id,String name,String email,Role role,String address,String phoneNo){static ProfileView of(AppUser u){return new ProfileView(u.id,u.fullName,u.email,u.role,u.address,u.phoneNo);}}
}

@Service class ApprovedErService {
 private final Properties properties;private final PermitRecords applications;
 ApprovedErService(Properties p,PermitRecords a){properties=p;applications=a;}
 @Transactional void attach(PermitRecord p,PermitDtos.ApplicationInput d,AppUser actor){
  if(d.propertyId()!=null){
   PropertyRecord existing=properties.findById(d.propertyId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found."));
   boolean own=Objects.equals(existing.ownerId,actor.id)||applications.findByApplicantIdOrderByUpdatedAtDesc(actor.id).stream().anyMatch(a->Objects.equals(a.propertyId,existing.id));
   if(!own)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found.");
   p.propertyId=existing.id;p.propertyAddress=existing.address;
  }else {PropertyRecord property=new PropertyRecord(p.propertyAddress,d.property());property.ownerId=actor.id;properties.save(property);p.propertyId=property.id;}
 }
 boolean sameProperty(Long first,Long second){if(Objects.equals(first,second))return true;if(first==null||second==null)return true;PropertyRecord a=properties.findById(first).orElseThrow(),b=properties.findById(second).orElseThrow();return snapshot(a).equals(snapshot(b));}
 private List<Object> snapshot(PropertyRecord p){return Arrays.asList(p.address,p.streetNumber,p.streetDirection,p.streetName,p.zipCode,p.ward,p.communityType,p.latitude==null?null:p.latitude.stripTrailingZeros(),p.longitude==null?null:p.longitude.stripTrailingZeros());}
 ApprovedErModel.PropertyView property(Long id){return properties.findById(id).map(ApprovedErModel.PropertyView::of).orElseThrow();}
 @Transactional(readOnly=true) List<ApprovedErModel.PropertyView> owned(AppUser user){return properties.findByOwnerIdOrderById(user.id).stream().map(ApprovedErModel.PropertyView::of).toList();}
 @Transactional ApprovedErModel.PropertyView create(ApprovedErModel.PropertyCreate d,AppUser user){if(user.role!=Role.APPLICANT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only applicants can create properties.");PropertyRecord p=new PropertyRecord(d.address().trim(),d.details());p.ownerId=user.id;return ApprovedErModel.PropertyView.of(properties.save(p));}
}
