/**
 * Injectable wrapper for front-end config rendered as JavaScript literals in the index.html host page.
 */
import {Injectable} from '@angular/core';
// TODO import {window} from '@angular/platform-browser/src/facade/browser';

@Injectable()
export class ConfigService {

    private _version: String;
    private _apiPrefix: String;

    constructor() {
        this._apiPrefix = ''; // TODO window['CODEKVAST_API'] || '';
        this._version = 'unknown'; // window['CODEKVAST_VERSION'] || 'unknown';
    }

    getVersion(): String {
        return this._version;
    }

    getApiPrefix(): String {
        return this._apiPrefix;
    }
}
