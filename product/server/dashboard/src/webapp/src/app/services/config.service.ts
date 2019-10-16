/**
 * Injectable wrapper for front-end config rendered as JavaScript literals in the index.html host page.
 */
import {Injectable} from '@angular/core';
import {ServerSettings} from '../model/server-settings';
import {environment} from '../../environments/environment';

@Injectable()
export class ConfigService {

    private serverSettings = new ServerSettings();
    // noinspection TypeScriptFieldCanBeMadeReadonly
    private version = 'dev';

    constructor() {
        this.version = environment.codekvastVersion;
    }

    getVersion(): String {
        return this.version;
    }

    getServerSettings() {
        return this.serverSettings;
    }

    setServerSettings(serverSettings: ServerSettings) {
        console.log('[ck dashboard] ServerSettings=%o', serverSettings);
        this.serverSettings = serverSettings;
    }
}
