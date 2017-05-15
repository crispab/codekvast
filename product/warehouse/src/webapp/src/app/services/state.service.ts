/**
 * Injectable holder for persistent state, such as form content, search results etc.
 */
import {Injectable} from '@angular/core';

@Injectable()
export class StateService {

    state = {};

    getState<T>(key: string, initialState: () => T): T {
        if (!this.state.hasOwnProperty(key)) {
            this.state[key] = initialState();
        }
        return this.state[key];
    }

}
