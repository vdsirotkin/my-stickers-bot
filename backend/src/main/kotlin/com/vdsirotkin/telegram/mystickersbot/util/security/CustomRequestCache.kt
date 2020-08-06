package com.vdsirotkin.telegram.mystickersbot.util.security

import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * HttpSessionRequestCache that avoids saving internal framework requests.
 */
internal class CustomRequestCache : HttpSessionRequestCache() {
    /**
     * {@inheritDoc}
     *
     * If the method is considered an internal request from the framework, we skip
     * saving it.
     *
     * @see SecurityUtils.isFrameworkInternalRequest
     */
    override fun saveRequest(request: HttpServletRequest, response: HttpServletResponse) {
        if (!SecurityUtils.isFrameworkInternalRequest(request)) {
            super.saveRequest(request, response)
        }
    }
}
