/**
 * Injectable wrapper for front-end config rendered as JavaScript literals in the index.html host page.
 */
import {Injectable} from '@angular/core';

@Injectable()
export class ConfigService {

    getVersion(): String {
        return window['CODEKVAST_VERSION'] || 'dev';
    }

    getApiPrefix(): String {
        return window['CODEKVAST_API'] || '';
    }
}
