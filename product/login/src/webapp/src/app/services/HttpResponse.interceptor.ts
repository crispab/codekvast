import 'rxjs/add/operator/do';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponseBase} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Router} from '@angular/router';

@Injectable()
export class HttpResponseInterceptor implements HttpInterceptor {

    constructor(private router: Router) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next
            .handle(req.clone({
                withCredentials: true
            }))
            .do(event => {
                console.log('[ck login] HttpResponseInterceptor: response=%o', event);
                if (event instanceof HttpResponseBase) {
                    const response = event as HttpResponseBase;
                    if (response.status === 401 || response.status === 403) {
                        // noinspection JSIgnoredPromiseFromCall
                        this.router.navigateByUrl('forbidden');
                    }
                }
            });
    }
}
