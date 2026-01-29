package io.github.rivon0507.courier.auth.web.error;

public class InvalidDeviceIdException extends InvalidSessionException {
    public InvalidDeviceIdException() {
        super("malformed device_id");
    }
}
