import {ConfigService} from './config.service';
import {Headers, Http} from '@angular/http';
import {Injectable} from '@angular/core';
import {MethodData} from '../model/MethodData';
import {Method} from '../model/Method';
import {Observable} from 'rxjs/Observable';
import '../../rxjs-operators';
import {isNumber} from 'util';
import {StateService} from './state.service';
import {Router} from '@angular/router';
import 'rxjs/add/observable/of';

@Injectable()
export class WarehouseService {

    readonly METHODS_URL = '/webapp/v1/methods';
    readonly METHOD_BY_ID_URL = '/webapp/v1/method/detail/';
    readonly RENEW_AUTH_TOKEN_URL = '/webapp/renewAuthToken';
    readonly IS_DEMO_MODE_URL = '/webapp/isDemoMode';
    readonly AUTH_TOKEN_HEADER = 'X-Codekvast-Auth-Token';

    constructor(private http: Http, private configService: ConfigService, private stateService: StateService, private router: Router) {
    }

    getMethods(signature?: string, maxResults?: number): Observable<MethodData> {
        if (signature === '-----' && this.configService.getVersion() === 'dev') {
            console.log('Returning a canned response');
            return new Observable<MethodData>(subscriber => subscriber.next(require('../test/canned/v1/MethodData.json')));
        }

        const url: string = this.constructGetMethodsUrl(signature, maxResults);
        console.log('url=%s', url);
        return this.http.get(url, {headers: this.getHeaders()})
                   .do(res => this.replaceAuthToken(res), res => this.handleErrors(res))
                   .map(res => res.json());
    }

    constructGetMethodsUrl(signature: string, maxResults: number): string {
        let result = this.configService.getApiPrefix() + this.METHODS_URL;
        let delimiter = '?';
        if (signature !== undefined && signature.trim().length > 0) {
            result += `${delimiter}signature=${signature.replace('#', '.')}`;
            delimiter = '&';
        }
        if (isNumber(maxResults)) {
            result += `${delimiter}maxResults=${maxResults}`;
            delimiter = '&';
        }
        return result;
    }

    getMethodById(id: number): Observable<Method> {
        const url = this.constructGetMethodByIdUrl(id);
        console.log('url=%s', url);
        return this.http.get(url, {headers: this.getHeaders()})
                   .do(res => this.replaceAuthToken(res), res => this.handleErrors(res))
                   .map(res => res.json());
    }

    ping(): Observable<boolean> {
        if (this.stateService.getAuthToken() !== null) {
            return this.http.get(this.configService.getApiPrefix() + this.RENEW_AUTH_TOKEN_URL, {headers: this.getHeaders()})
                       .do(res => console.log('ping: %o', res))
                       .do(res => this.replaceAuthToken(res), res => this.handleErrors(res))
                       .map(() => true);
        }
        return Observable.of(true);
    }

    isDemoMode(): Observable<boolean> {
        return this.http.get(this.configService.getApiPrefix() + this.IS_DEMO_MODE_URL)
                   .do(res => console.log('isDemoMode: %o', res))
                   .map(res => res.text() === 'true');
    }

    constructGetMethodByIdUrl(id: number) {
        return this.configService.getApiPrefix() + this.METHOD_BY_ID_URL + id;
    }

    private getHeaders() {
        let headers = new Headers();
        headers.set('Content-type', 'application/json; charset=utf-8');
        headers.set('Authorization', 'Bearer ' + this.stateService.getAuthToken());
        return headers;
    }

    private replaceAuthToken(res: any) {
        return this.stateService.replaceAuthToken(res.headers.get(this.AUTH_TOKEN_HEADER));
    }

    private handleErrors(res: any) {
        console.log('Error=%o', res);

        let nextRoute = [''];

        if (res.status === 401) {
            if (this.stateService.isLoggedIn()) {
                // Bearer token time-out
                let loginState = this.stateService.getLoginState();
                nextRoute = ['/logged-out', loginState.source, loginState.sourceApp || 'unknown'];
            }
            this.stateService.setLoggedOut();
        }

        // noinspection JSIgnoredPromiseFromCall
        this.router.navigate(nextRoute);
    }

}
