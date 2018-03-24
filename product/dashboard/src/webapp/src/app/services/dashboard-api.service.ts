import {ConfigService} from './config.service';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {isNumber} from 'util';
import {MethodData} from '../model/methods/MethodData';
import {Method} from '../model/methods/Method';
import {Observable} from 'rxjs/Observable';
import {StatusData} from '../model/status/StatusData';

export class GetMethodsRequest {
    signature: string;
    maxResults: number;
    collectedDays: number;
    suppressSyntheticMethods: boolean;
    suppressUntrackedMethods: boolean;
    invokedBeforeMillis: number
}

@Injectable()
export class DashboardApiService {

    readonly METHOD_BY_ID_URL = '/dashboard/api/v1/method/detail/';
    readonly METHODS_URL = '/dashboard/api/v1/methods';
    readonly STATUS_URL = '/dashboard/api/v1/status';
    readonly LOGIN_URL = '/dashboard/api/v1/loginUrl';
    readonly LOGOUT_URL = '/dashboard/logout';

    constructor(private http: HttpClient, private configService: ConfigService) {
    }

    getMethods(req: GetMethodsRequest): Observable<MethodData> {
        if (req.signature === '-----' && this.configService.getVersion() === 'dev') {
            console.log('[ck dashboard] Returning a canned response');
            return new Observable<MethodData>(subscriber => subscriber.next(require('../test/canned/v1/MethodData.json')));
        }

        const url: string = this.constructGetMethodsUrl(req);

        return this.http.get<MethodData>(url, );
    }

    constructGetMethodsUrl(req: GetMethodsRequest): string {
        let result = this.METHODS_URL;
        let delimiter = '?';
        if (req.signature !== undefined && req.signature.trim().length > 0) {
            result += `${delimiter}signature=${encodeURI(req.signature)}`;
            delimiter = '&';
        }
        if (isNumber(req.maxResults)) {
            result += `${delimiter}maxResults=${req.maxResults}`;
            delimiter = '&';
        }
        if (isNumber(req.collectedDays)) {
            result += `${delimiter}minCollectedDays=${req.collectedDays}`;
            delimiter = '&';
        }
        if (req.suppressSyntheticMethods !== undefined) {
            result += `${delimiter}suppressSyntheticMethods=${req.suppressSyntheticMethods}`;
            delimiter = '&';
        }
        if (req.suppressUntrackedMethods !== undefined) {
            result += `${delimiter}suppressUntrackedMethods=${req.suppressUntrackedMethods}`;
            delimiter = '&';
        }
        if (isNumber(req.invokedBeforeMillis)) {
            result += `${delimiter}onlyInvokedBeforeMillis=${req.invokedBeforeMillis}`;
            delimiter = '&';
        }
        return result;
    }

    getMethodById(id: number): Observable<Method> {
        const url = this.constructGetMethodByIdUrl(id);
        return this.http.get<Method>(url);
    }

    getStatus(): Observable<StatusData> {
        return this.http.get<StatusData>(this.STATUS_URL);
    }

    constructGetMethodByIdUrl(id: number) {
        return this.METHOD_BY_ID_URL + id;
    }

    logout() {
        return this.http.post<any>(this.LOGOUT_URL, {}).subscribe( next => window.location.href = next);
    }

    getLoginUrl() {
        return this.http.get<string>(this.LOGIN_URL);
    }
}
