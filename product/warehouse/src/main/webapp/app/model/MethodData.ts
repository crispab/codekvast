import {Method} from './Method';
export class MethodData {
    timestamp: number;
    request: Object;
    queryTimeMillis: number;
    numMethods: number;
    methods: Method[];

    public computeFields() {
        for (let i = 0; i < this.methods.length; i++) {
            this.methods[i].computeFields();
        }
    }
}
