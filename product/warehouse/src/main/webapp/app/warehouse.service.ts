import {Injectable} from 'angular2/core';
import {Http, Headers, RequestOptions, Response} from 'angular2/http';
import 'rxjs/Rx';
import {Observable} from 'rxjs/Observable';
import {ConfigService} from './config.service';
import {Method} from './model/Method';

@Injectable()
export class WarehouseService {

    private methodsUrl = 'api/v1/methods';
    opts: RequestOptions;

    constructor(private http: Http, private configService: ConfigService) {
        var headers: Headers = new Headers();
        headers.append('content-type', 'application/json; charset=utf-8');
        this.opts = new RequestOptions();
        this.opts.headers = headers;
    }

    getMethods(signature: string): Observable<Method[]> {
        return this.http.get(this.constructMethodsUrl(signature))
            .map(this.extractMethodData)
            .catch(this.handleError);
    }

    private constructMethodsUrl(signature: string): string {
        let params = signature ? `?signature=${signature}` : '';
        return this.configService.getApiPrefix() + this.methodsUrl + params;
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
