package com.vdsirotkin.telegram.mystickersbot.config

import com.vdsirotkin.telegram.mystickersbot.util.security.CustomRequestCache
import com.vdsirotkin.telegram.mystickersbot.util.security.SecurityUtils
import com.vdsirotkin.telegram.mystickersbot.web.SEARCH_USER
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.util.matcher.RequestMatcher

@Configuration
class SecurityConfiguration : WebSecurityConfigurerAdapter() {
    /**
     * Require login to access internal pages and configure login form.
     */
    override fun configure(http: HttpSecurity) {
        // Not using Spring CSRF here to be able to use plain HTML for the login page
        http.csrf().disable() // Register our CustomRequestCache, that saves unauthorized access attempts, so
                // the user is redirected after login.
                .requestCache().requestCache(CustomRequestCache()) // Restrict access to our application.
                .and().authorizeRequests() // Allow all flow internal requests.
                .requestMatchers(RequestMatcher(SecurityUtils::isFrameworkInternalRequest)).permitAll() // Allow all requests by logged in users.
                .anyRequest().authenticated() // Configure the login page.
                .and().formLogin().loginPage(LOGIN_URL).permitAll().loginProcessingUrl(LOGIN_PROCESSING_URL).defaultSuccessUrl("/$SEARCH_USER")
                .failureUrl(LOGIN_FAILURE_URL) // Configure logout
                .and().logout().logoutSuccessUrl(LOGOUT_SUCCESS_URL)
    }

    /**
     * Allows access to static resources, bypassing Spring security.
     */
    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers( // Vaadin Flow static resources
                "/VAADIN/**",  // the standard favicon URI
                "/favicon.ico",  // the robots exclusion standard
                "/robots.txt",  // web application manifest
                "/manifest.webmanifest",
                "/sw.js",
                "/offline-page.html",  // icons and images
                "/icons/**",
                "/images/**",  // (development mode) static resources
                "/frontend/**",  // (development mode) webjars
                "/webjars/**",  // (development mode) H2 debugging console
                "/h2-console/**",  // (production mode) static resources
                "/frontend-es5/**", "/frontend-es6/**")
    }

    companion object {
        private const val LOGIN_PROCESSING_URL = "/login"
        private const val LOGIN_FAILURE_URL = "/login?error"
        private const val LOGIN_URL = "/login"
        private const val LOGOUT_SUCCESS_URL = "/login"
    }
}
