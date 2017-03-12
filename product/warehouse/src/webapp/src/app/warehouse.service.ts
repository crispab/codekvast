import {Injectable} from '@angular/core';
import {Http, Headers, Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {ConfigService} from './config.service';
import {MethodData} from './model/MethodData';
import '../rxjs-operators';

@Injectable()
export class WarehouseService {

    private methodsUrl = '/api/v1/methods';
    private headers = new Headers();

    constructor(private http: Http, private configService: ConfigService) {
        this.headers.append('content-type', 'application/json; charset=utf-8');
    }

    getMethods(signature?: string, maxResults?: number): Observable<MethodData> {
        const search: string = this.constructGetMethodsSearch(signature, maxResults);
        let url: string = this.constructUrl();
        if (search.length > 0) {
            url = url + '?' + search;
        }
        console.log('url=%s', url);
        return this.http.get(url, { headers: this.headers})
                   .map(this.extractMethodData)
                   .catch(this.handleError);
    }

    constructUrl(): string {
        return this.configService.getApiPrefix() + this.methodsUrl;
    }

    constructGetMethodsSearch(signature: string, maxResults: number): string {
        let search = '';
        let delimiter = '';
        if (signature !== undefined && signature.trim().length > 0) {
            search += `${delimiter}signature=${signature}`;
            delimiter = '&';
        }
        if (maxResults !== undefined) {
            search += `${delimiter}maxResults=${maxResults}`;
            delimiter = '&';
        }
        return search;
    }

    private extractMethodData(res: Response): MethodData {
        if (res.status < 200 || res.status >= 300) {
            throw new Error('Response status: ' + res.status);
        }

        return res.json();
    }

    private handleError(error: any) {
        let errMsg = error.message || JSON.stringify(error);
        console.error(errMsg);
        return Observable.throw(errMsg);
    }
}
