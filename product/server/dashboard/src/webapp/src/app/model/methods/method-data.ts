import {Method} from './method';

export class MethodData {
    timestamp: number;
    request: object;
    queryTimeMillis: number;
    numMethods: number;
    methods: Method[];
}
