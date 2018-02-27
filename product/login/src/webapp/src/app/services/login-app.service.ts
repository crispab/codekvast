import {ConfigService} from './config.service';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Router} from '@angular/router';
import {User} from '../model/User';

@Injectable()
export class LoginAppService {

    readonly HEADERS = new HttpHeaders().set('Content-type', 'application/json; charset=utf-8');

    constructor(private http: HttpClient, private configService: ConfigService, private router: Router) {
    }

    getUser(): Observable<User> {
        const url = this.configService.getApiPrefix() + '/user';
        return this.http.get<User>(url, {headers: this.HEADERS});
    }

    isAuthenticated(): Observable<boolean> {
        const url = this.configService.getApiPrefix() + '/authenticated';
        return this.http.get<boolean>(url, {headers: this.HEADERS});
    }

    logout(): void {
        const url = this.configService.getApiPrefix() + '/logout';
        this.http.post(url, {})
            .finally(() => {
                // noinspection JSIgnoredPromiseFromCall
                this.router.navigateByUrl('login');
            }).subscribe();
    }

}
