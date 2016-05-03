import {AppComponent} from './app.component';
import {ConfigService} from "./config.service";

describe('AppComponent', () => {

    let config: ConfigService;
    let app: AppComponent;

    beforeEach(() => {
        config = new ConfigService();
        spyOn(config, 'getApiPrefix').and.returnValue('somePrefix');
        spyOn(config, 'getVersion').and.returnValue('someVersion');
        app = new AppComponent(config);
    });

    it('app.apiPrefix() should return value of configService.apiPrefix()', () => expect(app.apiPrefix()).toEqual('somePrefix'));

    it('app.version() should return value of configService.version()', () => expect(app.version()).toEqual('someVersion'))
});
