package ee.openeid.siva.xroad;

import ee.openeid.siva.validation.exception.MalformedDocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class XROADValidationExceptionHandler {

    private MessageSource messageSource;

    @ExceptionHandler(MalformedDocumentException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErroneousResponse handleMalformedDocumentException(MalformedDocumentException e) {
        return new ErroneousResponse("document", messageSource.getMessage("error.document.malformed", null, null));
    }

    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
}