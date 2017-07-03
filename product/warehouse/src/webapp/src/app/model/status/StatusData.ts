import {Agent} from './Agent';
import {User} from './User';

export class StatusData {
    // query stuff
    timestamp: number;
    queryTimeMillis: number;

    // price plan stuff
    pricePlan: string;
    collectionResolutionSeconds: number;
    maxNumberOfAgents: number;
    maxNumberOfMethods: number;
    maxCollectionPeriodDays: number;

    // actual values
    numMethods: number;
    collectedSinceMillis: number;
    collectedDays: number;
    numAgents: number;
    numLiveAgents: number;
    numLiveEnabledAgents: number;

    // details
    agents: Agent[];
    users: User[];
}
