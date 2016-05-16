import {Injectable} from 'angular2/core';
import {Http, Headers, Response} from 'angular2/http';
import 'rxjs/Rx';
import {Observable} from 'rxjs/Observable';
import {ConfigService} from './config.service';
import {Method} from './model/Method';

@Injectable()
export class WarehouseService {

    private methodsUrl = 'api/v1/methods';
    private headers = new Headers();

    constructor(private http: Http, private configService: ConfigService) {
        this.headers.append('content-type', 'application/json; charset=utf-8');
    }

    getMethods(signature?: string, maxResults?: number): Observable<Method[]> {
        return this.http.get(this.constructGetMethodsUrl(signature, maxResults), {headers: this.headers})
            .map(this.extractMethodData)
            .catch(this.handleError);
    }

    constructGetMethodsUrl(signature: string, maxResults: number): string {
        let url = this.configService.getApiPrefix() + this.methodsUrl;
        let delimiter = '?';
        if (signature !== undefined && signature.length > 0) {
            url += `${delimiter}signature=${signature}`;
            delimiter = '&';
        }
        if (maxResults !== undefined) {
            url += `${delimiter}maxResults=${maxResults}`;
            delimiter = '&';
        }
        return  url;
    }

    private extractMethodData(res: Response) {
        if (res.status < 200 || res.status >= 300) {
            throw new Error('Response status: ' + res.status);
        }
        let body = res.json();
        return body.data || {};
    }

    private handleError(error: any) {
        let errMsg = error.message || 'Server error';
        console.error(errMsg); // log to console instead
        return Observable.throw(errMsg);
    }
}
