import {HttpErrorResponse, HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest, HttpResponse} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {Router} from '@angular/router';
import {StateService} from './state.service';
import {CookieService} from 'ngx-cookie';
import {tap} from 'rxjs/operators';

@Injectable()
export class HttpResponseInterceptor implements HttpInterceptor {

    constructor(private stateService: StateService, private router: Router, private cookieService: CookieService) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next
            .handle(req.clone({
                headers: new HttpHeaders()
                    .set('Content-type', 'application/json; charset=utf-8')
                    .set('X-XSRF-TOKEN', this.cookieService.get('XSRF-TOKEN'))
            })).pipe(tap(event => {
                if (event instanceof HttpResponse) {
                    console.log('[ck dashboard] HttpResponse=%o', event);
                }
            }, err => {
                if (err instanceof HttpErrorResponse) {
                    console.log('[ck dashboard] HttpErrorResponse=%o', err);
                    if (err.status === 401 || err.status === 403) {
                        this.stateService.setLoggedOut();

                        // noinspection JSIgnoredPromiseFromCall
                        this.router.navigate(['/not-logged-in']);
                    }
                }
            }));
    }
}
