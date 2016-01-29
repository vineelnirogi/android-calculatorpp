/*
 * Copyright 2013 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.calculator.math.edit;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import jscl.math.function.Function;
import jscl.math.function.IFunction;
import org.solovyev.android.Check;
import org.solovyev.android.calculator.*;
import org.solovyev.android.calculator.entities.Category;
import org.solovyev.android.calculator.function.CppFunction;
import org.solovyev.android.calculator.function.EditFunctionFragment;
import org.solovyev.android.calculator.function.FunctionRemovalDialog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class FunctionsFragment extends BaseEntitiesFragment<Function> {

    public static final String ARG_FUNCTION = "function";
    @Inject
    FunctionsRegistry registry;
    @Inject
    Calculator calculator;
    @Inject
    Keyboard keyboard;
    @Inject
    Bus bus;

    public FunctionsFragment() {
        super(CalculatorFragmentType.functions);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getArguments();
        if (bundle != null) {
            final CppFunction function = bundle.getParcelable(ARG_FUNCTION);
            if (function != null) {
                EditFunctionFragment.showDialog(function, getFragmentManager());
                // don't want to show it again
                bundle.remove(ARG_FUNCTION);
            }
        }
    }

    @Override
    protected void inject(@Nonnull AppComponent component) {
        super.inject(component);
        component.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        bus.register(this);
        return view;
    }

    @Override
    public void onViewCreated(View root, Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        fab.setVisibility(View.VISIBLE);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditFunctionFragment.showDialog(getActivity());
            }
        });
    }

    @Override
    protected void onClick(@Nonnull Function function) {
        keyboard.buttonPressed(function.getName());
        final FragmentActivity activity = getActivity();
        if (activity instanceof FunctionsActivity) {
            activity.finish();
        }
    }

    @Override
    protected void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull Function function, @NonNull MenuItem.OnMenuItemClickListener listener) {
        addMenu(menu, R.string.c_use, listener);
        if (!function.isSystem()) {
            addMenu(menu, R.string.c_edit, listener);
            addMenu(menu, R.string.c_remove, listener);
        }
    }

    @Override
    protected boolean onMenuItemClicked(@Nonnull MenuItem item, @Nonnull final Function function) {
        final FragmentActivity activity = getActivity();
        switch (item.getItemId()) {
            case R.string.c_use:
                onClick(function);
                return true;
            case R.string.c_edit:
                if (function instanceof IFunction) {
                    EditFunctionFragment.showDialog(CppFunction.builder((IFunction) function).build(), activity.getSupportFragmentManager());
                }
                return true;
            case R.string.c_remove:
                FunctionRemovalDialog.show(getActivity(), function.getName(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Check.isTrue(which == DialogInterface.BUTTON_POSITIVE);
                        registry.remove(function);
                    }
                });
                return true;
        }
        return false;
    }

    @Nonnull
    @Override
    protected List<Function> getEntities() {
        return new ArrayList<>(registry.getEntities());
    }

    @Override
    protected Category getCategory(@Nonnull Function function) {
        return registry.getCategory(function);
    }

    @Override
    public void onDestroyView() {
        bus.unregister(this);
        super.onDestroyView();
    }

    @Subscribe
    public void onFunctionAdded(@NonNull final FunctionsRegistry.AddedEvent event) {
        onEntityAdded(event.function);
    }

    @Subscribe
    public void onFunctionChanged(@NonNull final FunctionsRegistry.ChangedEvent event) {
        onEntityChanged(event.newFunction);
    }

    @Subscribe
    public void onFunctionRemoved(@NonNull final FunctionsRegistry.RemovedEvent event) {
        onEntityRemoved(event.function);
    }

    @Nullable
    @Override
    protected String getDescription(@NonNull Function function) {
        return registry.getDescription(function.getName());
    }
}
