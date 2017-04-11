import {ConfigService} from './config.service';
import {Headers, Http, Response} from '@angular/http';
import {Injectable} from '@angular/core';
import {MethodData} from './model/MethodData';
import {Method} from './model/Method';
import {Observable} from 'rxjs/Observable';
import '../rxjs-operators';

@Injectable()
export class WarehouseService {

    readonly METHODS_URL = '/api/v1/methods';
    readonly METHOD_BY_ID_URL = '/api/v1/method/detail/';
    readonly headers = new Headers();

    constructor(private http: Http, private configService: ConfigService) {
        this.headers.append('content-type', 'application/json; charset=utf-8');
    }

    getMethods(signature?: string, maxResults?: number): Observable<MethodData> {
        if (signature === '-----' && this.configService.getVersion() === 'dev') {
            console.log('Returning a canned response');
            return new Observable<MethodData>(subscriber => subscriber.next(require('./test/canned/v1/MethodData.json')));
        }

        const url: string = this.constructGetMethodsUrl(signature, maxResults);
        console.log('url=%s', url);
        return this.http.get(url, { headers: this.headers})
                   .map(this.extractMethodsData)
                   .catch(this.handleError);
    }

    constructGetMethodsUrl(signature: string, maxResults: number): string {
        let result = this.configService.getApiPrefix() + this.METHODS_URL;
        let delimiter = '?';
        if (signature !== undefined && signature.trim().length > 0) {
            result += `${delimiter}signature=${signature.replace('#', '.')}`;
            delimiter = '&';
        }
        if (maxResults !== undefined) {
            result += `${delimiter}maxResults=${maxResults}`;
            delimiter = '&';
        }
        return result;
    }

    private extractMethodsData(res: Response): MethodData {
        if (res.status < 200 || res.status >= 300) {
            throw new Error('Response status: ' + res.status);
        }

        return res.json();
    }

    private handleError(error: any) {
        // TODO: improve error handling
        let errMsg = error.message || JSON.stringify(error);
        console.error(errMsg);
        return Observable.throw(errMsg);
    }

    getMethodById(id: number): Observable<Method> {
        const url = this.constructGetMethodByIdUrl(id);
        console.log('url=%s', url);
        return this.http.get(url)
                   .map(this.extractMethod)
                   .catch(this.handleError);
    }


    constructGetMethodByIdUrl(id: number) {
        return this.configService.getApiPrefix() + this.METHOD_BY_ID_URL + id;
    }

    private extractMethod(res: Response): Method {
        if (res.status < 200 || res.status >= 300) {
            throw new Error('Response status: ' + res.status);
        }

        return res.json();
    }

}
