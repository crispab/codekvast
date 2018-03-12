import 'rxjs/add/operator/do';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Router} from '@angular/router';

@Injectable()
export class HttpResponseInterceptor implements HttpInterceptor {

    constructor(private router: Router) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const xhr = req.clone({
            headers: req.headers.set('X-Requested-With', 'XMLHttpRequest')
        });
        return next
            .handle(xhr)
            .do(event => {
                if (event instanceof HttpResponse) {
                    console.log('[ck] HttpResponse=%o', event);
                }
            }, err => {
                console.log('[ck] HttpResponseInterceptor: err=%o', err);
                if ((err instanceof HttpErrorResponse) && (err.status === 401 || err.status === 403)) {
                    // noinspection JSIgnoredPromiseFromCall
                    this.router.navigateByUrl('forbidden');
                }
            });
    }
}
