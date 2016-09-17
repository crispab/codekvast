/**
 * Injectable wrapper for front-end config rendered as JavaScript literals in the index.html host page.
 */
import {Injectable} from '@angular/core';
import {DOCUMENT} from '@angular/platform-browser';

@Injectable()
export class ConfigService {

    private _version: String;
    private _apiPrefix: String;

    constructor() {
        this._apiPrefix = DOCUMENT['CODEKVAST_API'] || '';
        this._version = DOCUMENT['CODEKVAST_VERSION'] || 'unknown';
    }

    getVersion(): String {
        return this._version;
    }

    getApiPrefix(): String {
        return this._apiPrefix;
    }
}
