import {Environment} from './Environment';
import {Application} from './Application';

export class Method {
    collectedDays: number;
    collectedInEnvironments: Environment[];
    collectedSinceMillis: number;
    collectedToMillis: number;
    declaringType: string;
    id: number;
    lastInvokedAtMillis: number;
    modifiers: string;
    occursInApplications: Application[];
    packageName: string;
    signature: string;
    tags: string[];
    visibility: string;

    // computed fields
    collectedSince: Date;
    collectedTo: Date;
    lastInvokedAt: Date;

    public computeFields() {
        if (this.collectedSinceMillis > 0) {
            this.collectedSince = new Date(this.collectedSinceMillis);
        }
        if (this.collectedToMillis > 0) {
            this.collectedTo = new Date(this.collectedToMillis);
        }
        if (this.lastInvokedAtMillis > 0) {
            this.lastInvokedAt = new Date(this.lastInvokedAtMillis);
        }
    }
}
