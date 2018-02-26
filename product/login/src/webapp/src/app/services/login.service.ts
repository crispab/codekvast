import {ConfigService} from './config.service';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {User} from '../model/User';

@Injectable()
export class LoginService {

    readonly HEADERS = new HttpHeaders().set('Content-type', 'application/json; charset=utf-8');

    constructor(private http: HttpClient, private configService: ConfigService) {
    }

    getUser(): Observable<User> {
        const url = this.configService.getApiPrefix() + '/user';
        return this.http.get<User>(url, {headers: this.HEADERS});
    }

    isLoggedIn(): Observable<boolean> {
        const url = this.configService.getApiPrefix() + '/is-logged-in';
        return this.http.get<boolean>(url, {headers: this.HEADERS});
    }

}
