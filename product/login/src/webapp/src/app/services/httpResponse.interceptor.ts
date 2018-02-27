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
        return next
            .handle(req)
            .do(event => {
                if (event instanceof HttpResponse) {
                    console.log('HttpResponse=%o', event);
                }
            }, err => {
                if (err instanceof HttpErrorResponse) {
                    console.log('HttpErrorResponse=%o', err);
                    if (err.status === 401) {
                        // noinspection JSIgnoredPromiseFromCall
                        this.router.navigateByUrl('login');
                    }
                }
            });
    }
}
