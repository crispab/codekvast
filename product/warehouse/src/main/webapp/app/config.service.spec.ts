import {window} from "angular2/src/facade/browser";
import {ConfigService} from "./config.service";

describe('ConfigService', () => {

    it('config.getApiPrefix() should return value of window.CODEKVAST_API when defined',
        () => {
            window['CODEKVAST_API'] = 'somePrefix';
            expect(new ConfigService().getApiPrefix()).toEqual('somePrefix')
        });

    it('config.getApiPrefix() should return empty string when undefined',
        () => {
            window['CODEKVAST_API'] = undefined;
            expect(new ConfigService().getApiPrefix()).toEqual('')
        });

    it('config.getVersion() should return value of window.CODEKVAST_VERSION when defined',
        () => {
            window['CODEKVAST_VERSION'] = 'someVersion';
            expect(new ConfigService().getVersion()).toEqual('someVersion')
        })

    it('config.getVersion() should return "unknown" of window.CODEKVAST_VERSION when undefined',
        () => {
            window['CODEKVAST_VERSION'] = undefined;
            expect(new ConfigService().getVersion()).toEqual('unknown')
        })
});
