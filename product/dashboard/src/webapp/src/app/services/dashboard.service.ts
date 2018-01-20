import {ConfigService} from './config.service';
import {HttpClient, HttpHeaders} from '@angular/common/http';
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
export class DashboardService {

    readonly METHODS_URL = '/webapp/v1/methods';
    readonly METHOD_BY_ID_URL = '/webapp/v1/method/detail/';
    readonly STATUS_URL = '/webapp/v1/status';
    readonly IS_DEMO_MODE_URL = '/webapp/isDemoMode';
    readonly HEADERS = new HttpHeaders().set('Content-type', 'application/json; charset=utf-8');

    constructor(private http: HttpClient, private configService: ConfigService) {
    }

    getMethods(req: GetMethodsRequest): Observable<MethodData> {
        if (req.signature === '-----' && this.configService.getVersion() === 'dev') {
            console.log('Returning a canned response');
            return new Observable<MethodData>(subscriber => subscriber.next(require('../test/canned/v1/MethodData.json')));
        }

        const url: string = this.constructGetMethodsUrl(req);

        return this.http.get<MethodData>(url, {headers: this.HEADERS});
    }

    constructGetMethodsUrl(req: GetMethodsRequest): string {
        let result = this.configService.getApiPrefix() + this.METHODS_URL;
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
        console.log('GetMethodsUrl(%o) returns %o', req, result);
        return result;
    }

    getMethodById(id: number): Observable<Method> {
        const url = this.constructGetMethodByIdUrl(id);
        return this.http.get<Method>(url, {headers: this.HEADERS});
    }

    getStatus(): Observable<StatusData> {
        const url = this.configService.getApiPrefix() + this.STATUS_URL;
        return this.http.get<StatusData>(url, {headers: this.HEADERS});
    }

    isDemoMode(): Observable<boolean> {
        return this.http.get<boolean>(this.configService.getApiPrefix() + this.IS_DEMO_MODE_URL);
    }

    constructGetMethodByIdUrl(id: number) {
        return this.configService.getApiPrefix() + this.METHOD_BY_ID_URL + id;
    }

}
