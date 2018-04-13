/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch;

import java.io.IOException;

public abstract class ConchException extends Exception {

    protected ConchException() {
        super();
    }

    protected ConchException(String message) {
        super(message);
    }

    protected ConchException(String message, Throwable cause) {
        super(message, cause);
    }

    protected ConchException(Throwable cause) {
        super(cause);
    }

    public static abstract class ValidationException extends ConchException {

        private ValidationException(String message) {
            super(message);
        }

        private ValidationException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class NotCurrentlyValidException extends ValidationException {

        public NotCurrentlyValidException(String message) {
            super(message);
        }

        public NotCurrentlyValidException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class ExistingTransactionException extends NotCurrentlyValidException {

        public ExistingTransactionException(String message) {
            super(message);
        }

    }

    public static final class NotYetEnabledException extends NotCurrentlyValidException {

        public NotYetEnabledException(String message) {
            super(message);
        }

        public NotYetEnabledException(String message, Throwable throwable) {
            super(message, throwable);
        }

    }

    public static final class NotValidException extends ValidationException {

        public NotValidException(String message) {
            super(message);
        }

        public NotValidException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class AccountControlException extends NotCurrentlyValidException {

        public AccountControlException(String message) {
            super(message);
        }

        public AccountControlException(String message, Throwable cause) {
            super(message, cause);
        }
        
    }

    public static class InsufficientBalanceException extends NotCurrentlyValidException {

        public InsufficientBalanceException(String message) {
            super(message);
        }

        public InsufficientBalanceException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final class NotYetEncryptedException extends IllegalStateException {

        public NotYetEncryptedException(String message) {
            super(message);
        }

        public NotYetEncryptedException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final class StopException extends RuntimeException {

        public StopException(String message) {
            super(message);
        }

        public StopException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final class ConchIOException extends IOException {

        public ConchIOException(String message) {
            super(message);
        }

        public ConchIOException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
