import {Method} from './Method';
export class MethodData {
    timestamp: number;
    request: Object;
    queryTimeMillis: number;
    numMethods: number;
    methods: Method[];
}
