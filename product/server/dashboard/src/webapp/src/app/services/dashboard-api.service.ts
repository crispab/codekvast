import {MethodsFormData} from '../model/methods/methods-form-data';
import {GetMethodsRequest} from '../model/methods/get-methods-request';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {MethodData} from '../model/methods/method-data';
import {Method} from '../model/methods/method';
import {Observable} from 'rxjs';
import {ServerSettings} from '../model/server-settings';
import {StatusData} from '../model/status/status-data';

@Injectable()
export class DashboardApiService {

    readonly METHOD_BY_ID_URL = '/dashboard/api/v1/method/detail/';
    readonly METHODS_FORM_DATA_URL = '/dashboard/api/v1/methodsFormData';
    readonly METHODS_URL = '/dashboard/api/v2/methods';
    readonly SERVER_SETTINGS_URL = '/dashboard/api/v1/serverSettings';
    readonly STATUS_URL = '/dashboard/api/v1/status';

    constructor(private http: HttpClient) {
    }

    getMethodsFormData(): Observable<MethodsFormData> {
        return this.http.get<MethodsFormData>(this.METHODS_FORM_DATA_URL);
    }

    getMethods(req: GetMethodsRequest): Observable<MethodData> {
        console.log('[ck dashboard] getMethods(%o)', req);
        return this.http.post<MethodData>(this.METHODS_URL, JSON.stringify(req));
    }

    getMethodById(id: number): Observable<Method> {
        return this.http.get<Method>(this.METHOD_BY_ID_URL + id);
    }

    getStatus(): Observable<StatusData> {
        return this.http.get<StatusData>(this.STATUS_URL);
    }

    getServerSettings() {
        return this.http.get<ServerSettings>(this.SERVER_SETTINGS_URL);
    }
}
