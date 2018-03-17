import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Router} from '@angular/router';
import {User} from '../model/User';

@Injectable()
export class LoginApiService {

    readonly HEADERS = new HttpHeaders().set('Content-type', 'application/json; charset=utf-8');

    constructor(private http: HttpClient, private router: Router) {
    }

    getUser(): Observable<User> {
        return this.http.get<User>('/api/user', {headers: this.HEADERS});
    }

    isAuthenticated(): Observable<boolean> {
        return this.http.get<boolean>('/api/isAuthenticated', {headers: this.HEADERS});
    }

    logout(): void {
        this.http.post('/api/logout', {})
            .finally(() => {
                // noinspection JSIgnoredPromiseFromCall
                this.router.navigateByUrl('login');
            }).subscribe();
    }

    launchDashboard(customerId: number): void {
        this.http.post<string>(`/api/dashboard/launch/${customerId}`, {}, {headers: this.HEADERS})
            .do(location => console.log('[ck login] /api/dashboard/launch response=%o', location))
            .subscribe(location => this.http.post<string>(location, {}, {headers: this.HEADERS})
                                       .do(next => console.log(`[ck dashboard] response to ${location}=%o`, next))
                                       .subscribe(next => window.location.href = next));
    }

    getDashboardBaseUrl() {
        return this.http.get<string>('/api/dashboard/baseUrl');
    }
}
