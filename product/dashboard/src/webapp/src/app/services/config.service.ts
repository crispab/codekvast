/**
 * Injectable wrapper for front-end config rendered as JavaScript literals in the index.html host page.
 */
import {Injectable} from '@angular/core';
import {ServerSettings} from '../model/ServerSettings';

@Injectable()
export class ConfigService {

    private serverSettings = new ServerSettings();

    getVersion(): String {
        return window['CODEKVAST_VERSION'] || 'dev';
    }

    getServerSettings() {
        return this.serverSettings;
    }

    setServerSettings(serverSettings: ServerSettings) {
        console.log('[ck dashboard] ServerSettings=%o', serverSettings);
        this.serverSettings = serverSettings;
    }
}
