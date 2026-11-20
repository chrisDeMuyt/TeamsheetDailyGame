package com.teamsheet.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-IP request limits on the API: a general cap, plus tighter ones on guesses and the "Did you mean" search.
 * Over the limit = 429 with Retry-After. Static files (the Angular app) aren't limited.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    static final Duration MINUTE = Duration.ofMinutes(1);

    private final RateLimiter limiter;
    private final RateLimitProperties props;
    private final ObjectMapper json;

    public RateLimitFilter(RateLimiter limiter, RateLimitProperties props, ObjectMapper json) {
        this.limiter = limiter;
        this.props = props;
        this.json = json;
    }

    /** Fly's proxy sets Fly-Client-IP (the machine isn't reachable except through it); locally, the socket address. */
    public static String clientIp(HttpServletRequest req) {
        String fly = req.getHeader("Fly-Client-IP");
        return fly != null && !fly.isBlank() ? fly.trim() : req.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !props.enabled() || !req.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ip = clientIp(req);
        String path = req.getRequestURI();
        long wait = limiter.hit("api", ip, props.apiPerMinute(), MINUTE);
        if (wait == 0 && "POST".equals(req.getMethod()) && path.matches("/api/plays/[^/]+/guesses"))
            wait = limiter.hit("guess", ip, props.guessesPerMinute(), MINUTE);
        if (wait == 0 && path.equals("/api/players/suggest"))
            wait = limiter.hit("suggest", ip, props.suggestPerMinute(), MINUTE);
        if (wait > 0) {
            tooMany(res, wait, "Too many requests. Wait a moment and try again.");
            return;
        }
        chain.doFilter(req, res);
    }

    void tooMany(HttpServletResponse res, long retryAfterSeconds, String detail) throws IOException {
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(res.getOutputStream(), ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, detail));
    }
}
