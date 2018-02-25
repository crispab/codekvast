import {ConfigService} from './config.service';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';

export class GetMethodsRequest {
    signature: string;
    maxResults: number;
    collectedDays: number;
    suppressSyntheticMethods: boolean;
    suppressUntrackedMethods: boolean;
    invokedBeforeMillis: number
}

@Injectable()
export class LoginService {

    readonly IS_DEMO_MODE_URL = '/webapp/isDemoMode';
    readonly HEADERS = new HttpHeaders().set('Content-type', 'application/json; charset=utf-8');

    constructor(private http: HttpClient, private configService: ConfigService) {
    }

    isDemoMode(): Observable<boolean> {
        return this.http.get<boolean>(this.configService.getApiPrefix() + this.IS_DEMO_MODE_URL);
    }
}
