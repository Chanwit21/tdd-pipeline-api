package com.gable.tddpipeline.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Convenience accessor for the authenticated principal inside services/controllers. */
public final class CurrentUser {
    private CurrentUser() {}

    public static AppUserPrincipal get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal p)) {
            throw new UsernameNotFoundException("ไม่ได้เข้าสู่ระบบ");
        }
        return p;
    }
}
