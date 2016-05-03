/**
 * Created by olle on 2016-05-03.
 */
import {Injectable} from "angular2/core";
import {window} from 'angular2/src/facade/browser';

@Injectable()
export class ConfigService {

    private _version: String;
    private _apiPrefix: String;

    constructor() {
        this._apiPrefix = window['CODEKVAST_API'];
        this._version = window['CODEKVAST_VERSION'];
    }

    getVersion(): String {
        return this._version;
    }

    getApiPrefix(): String {
        return this._apiPrefix;
    }
}
