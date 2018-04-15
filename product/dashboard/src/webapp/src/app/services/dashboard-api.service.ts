import {MethodsFormData} from '../model/methods/MethodsFormData';
import {GetMethodsRequest} from '../model/methods/GetMethodsRequest';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {MethodData} from '../model/methods/MethodData';
import {Method} from '../model/methods/Method';
import {Observable} from 'rxjs/Observable';
import {ServerSettings} from '../model/ServerSettings';
import {StatusData} from '../model/status/StatusData';

@Injectable()
export class DashboardApiService {

    readonly DELETE_AGENT_URL = '/dashboard/api/v1/agent';
    readonly METHOD_BY_ID_URL = '/dashboard/api/v1/method/detail/';
    readonly METHODS_FORM_DATA_URL = '/dashboard/api/v1/methodsFormData';
    readonly METHODS_URL = '/dashboard/api/v1/methods';
    readonly SERVER_SETTINGS_URL = '/dashboard/api/v1/serverSettings';
    readonly STATUS_URL = '/dashboard/api/v1/status';

    constructor(private http: HttpClient) {
    }

    getMethodsFormData(): Observable<MethodsFormData> {
        return this.http.get<MethodsFormData>(this.METHODS_FORM_DATA_URL);
    }

    getMethods(req: GetMethodsRequest): Observable<MethodData> {
        if (req.signature === '-----' && window['CODEKVAST_VERSION'] === 'dev') {
            console.log('[ck dashboard] Returning a canned response');
            return new Observable<MethodData>(subscriber => subscriber.next(require('../test/canned/v1/MethodData.json')));
        }
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

    deleteAgent(agentId: number, jvmId: number) {
        return this.http.delete(`${this.DELETE_AGENT_URL}/${agentId}/${jvmId}`);
    }
}
