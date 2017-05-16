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

@Injectable()
export class WarehouseService {

    readonly METHODS_URL = '/webapp/v1/methods';
    readonly METHOD_BY_ID_URL = '/webapp/v1/method/detail/';
    readonly RENEW_AUTH_TOKEN_URL = '/webapp/renewAuthToken';
    readonly AUTH_TOKEN_HEADER = 'X-codekvast-auth-token';

    readonly headers = new Headers();

    constructor(private http: Http, private configService: ConfigService, private stateService: StateService,
                private router: Router) {
        this.headers.set('Content-type', 'application/json; charset=utf-8');
        this.headers.set('Authorization', 'Bearer ' + this.stateService.getAuthToken());
    }

    getMethods(signature?: string, maxResults?: number): Observable<MethodData> {
        if (signature === '-----' && this.configService.getVersion() === 'dev') {
            console.log('Returning a canned response');
            return new Observable<MethodData>(subscriber => subscriber.next(require('../test/canned/v1/MethodData.json')));
        }

        const url: string = this.constructGetMethodsUrl(signature, maxResults);
        console.log('url=%s', url);
        return this.http.get(url, {headers: this.headers})
                   .do(res => this.replaceAuthToken(res), res => this.handle401(res))
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
        return this.http.get(url, {headers: this.headers})
                   .do(res => this.replaceAuthToken(res), res => this.handle401(res))
                   .map(res => res.json());
    }

    ping(): void {
        this.http.get(this.RENEW_AUTH_TOKEN_URL, {headers: this.headers})
            .subscribe(res => this.replaceAuthToken(res), res => this.handle401(res));
    }


    constructGetMethodByIdUrl(id: number) {
        return this.configService.getApiPrefix() + this.METHOD_BY_ID_URL + id;
    }

    private replaceAuthToken(res: any) {
        return this.stateService.setAuthToken(res.headers.get(this.AUTH_TOKEN_HEADER));
    }

    private handle401(res: any) {
        if (res.status === 401) {
            this.stateService.setAuthToken(null);
            // TODO: handle status 401 Unauthorized
            this.router.navigate(['']);
        }
    }

}
