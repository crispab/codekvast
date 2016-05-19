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
}
