import {Method} from './method';

export class MethodData {
    timestamp: number;
    request: Object;
    queryTimeMillis: number;
    numMethods: number;
    methods: Method[];
}
