import {window} from "angular2/src/facade/browser";
import {ConfigService} from "./config.service";

describe('ConfigService', () => {

    let config: ConfigService;

    beforeEach(() => {
        window['CODEKVAST_API'] = 'somePrefix';
        window['CODEKVAST_VERSION'] = 'someVersion';
        
        config = new ConfigService();
    });

    it('config.getApiPrefix() should return value of window.CODEKVAST_API', () => expect(config.getApiPrefix()).toEqual('somePrefix'));

    it('config.getVersion() should return value of window.CODEKVAST_VERSION', () => expect(config.getVersion()).toEqual('someVersion'))
});
