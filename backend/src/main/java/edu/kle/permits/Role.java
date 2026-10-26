package edu.kle.permits;

import java.util.Set;
public enum Role {
 ADMIN(Set.of("users:manage","workflow:monitor")),
 APPLICANT(Set.of("applications:create","applications:own:read","applications:own:edit","applications:own:resubmit")),
 DEPARTMENT_OFFICER(Set.of("documents:verify","assignments:manage","workflow:monitor")),
 PERMIT_REVIEWER(Set.of("reviews:assigned:read","reviews:assigned:write")),
 ZONING_OFFICER(Set.of("zoning:assigned:read","zoning:assigned:write")),
 SENIOR_DEPARTMENT_OFFICER(Set.of("decisions:read","decisions:write")),
 FINANCE_OFFICER(Set.of("payments:read","payments:write")),
 BUILDING_INSPECTOR(Set.of("inspections:assigned:read","inspections:assigned:write"));
 public final Set<String> permissions;
 Role(Set<String> permissions){this.permissions=permissions;}
}
