import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {StatusComponent} from './status.component';
import {MethodsComponent} from './methods.component';

const routes: Routes = [
    {
        path: '',
        redirectTo: 'methods',
        pathMatch: 'full'
    }, {
        path: 'methods',
        component: MethodsComponent
    }, {
        path: 'status',
        component: StatusComponent
    }, {
        path: '**',
        redirectTo: 'methods',
        pathMatch: 'full'
    }
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
