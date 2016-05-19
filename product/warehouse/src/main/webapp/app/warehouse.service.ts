import {Injectable} from 'angular2/core';
import {Http, Headers, Response} from 'angular2/http';
import 'rxjs/Rx';
import {Observable} from 'rxjs/Observable';
import {ConfigService} from './config.service';
import {MethodData} from './model/MethodData';

@Injectable()
export class WarehouseService {

    private methodsUrl = '/api/v1/methods';
    private headers = new Headers();

    constructor(private http: Http, private configService: ConfigService) {
        this.headers.append('content-type', 'application/json; charset=utf-8');
    }

    getMethods(signature?: string, maxResults?: number): Observable<MethodData> {
        return this.http.get(this.constructGetMethodsUrl(signature, maxResults), {headers: this.headers})
            .map(this.extractMethodData)
            .map(this.computeFields, this)
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

    private makeDate(millis: number): Date {
        return millis === 0 ? undefined : new Date(millis);
    }

    private computeFields(data: MethodData): MethodData {

        data.methods.forEach((m) => {
            m.collectedSince = this.makeDate(m.collectedSinceMillis);
            m.collectedTo = this.makeDate(m.collectedToMillis);
            m.lastInvokedAt = this.makeDate(m.lastInvokedAtMillis);

            m.occursInApplications.forEach((a) => {
                a.dumpedAt = this.makeDate(a.dumpedAtMillis);
                a.invokedAt = this.makeDate(a.invokedAtMillis);
                a.startedAt = this.makeDate(a.startedAtMillis);
            });

            m.collectedInEnvironments.forEach((e) => {
                e.collectedSince = this.makeDate(e.collectedSinceMillis);
                e.collectedTo = this.makeDate(e.collectedSinceMillis);
                e.invokedAt = this.makeDate(e.invokedAtMillis);
            });
        });
        return data;
    }
}
