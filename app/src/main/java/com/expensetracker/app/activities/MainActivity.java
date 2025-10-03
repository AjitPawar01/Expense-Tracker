package com.expensetracker.app.activities;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.expensetracker.app.R;
import com.expensetracker.app.adapters.TransactionAdapter;
import com.expensetracker.app.models.Transaction;
import com.expensetracker.app.utils.FirebaseHelper;
import com.expensetracker.app.utils.BalanceCalculator;
import com.expensetracker.app.utils.FirebaseDebugHelper;
import com.expensetracker.app.models.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.appcompat.app.AlertDialog;
import com.expensetracker.app.utils.ThemeManager;

public class MainActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {
    private static final String TAG = "MainActivity";

    private TextView tvGreeting, tvWelcome, tvDate, tvOpeningBalance, tvClosingBalance;
    private TextView tvTotalIncome, tvTotalExpense, tvBalanceTrend, tvViewAll, tvNoExpenseData;
    private ImageView ivGreetingIcon;
    private RecyclerView rvRecentTransactions;
    private LinearLayout emptyRecentTransactions;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroupDateRange;
    private Chip chipToday, chipThisWeek, chipThisMonth;
    private CardView cardExpenseChart;
    private PieChart pieChartExpenses;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private User userProfile;
    private String startDate;
    private String endDate;
    private String yesterdayDate;
    private String userCompanyId;
    private String selectedRange = "today"; // today, week, month
    private List<Transaction> recentTransactions;
    private TransactionAdapter recentTransactionAdapter;

    private FloatingActionButton fabSpeedDial;
    private FrameLayout fabOverlay;
    private LinearLayout fabMenuLayout, fabAddIncome, fabAddExpense, fabSearchTransactions;
    private boolean isFabMenuOpen = false;
    private CardView cardBudgetProgress;
    private TextView tvSetBudget, tvBudgetSpent, tvBudgetRemaining, tvBudgetPercentage;
    private ProgressBar progressBudget;
    private double monthlyBudget = 0;

    // Chart
    private LineChart lineChartBalance;
    private TextView tvNoBalanceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.initializeTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity onCreate started");

        FirebaseDebugHelper.initializeFirebase();
        FirebaseDebugHelper.enableFirestoreLogging();
        FirebaseDebugHelper.testFirebaseConnection();

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        userCompanyId = getCompanyIdFromEmail(currentUser.getEmail());

        initViews();
        setupToolbar();
        setGreeting();
        setupSpeedDial();
        setupSwipeRefresh();
        setupDateRangeChips();
        setupRecentTransactions();
        updateDateRange("today");
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null && userCompanyId != null) {
            loadData();
        }
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvDate = findViewById(R.id.tvDate);
        ivGreetingIcon = findViewById(R.id.ivGreetingIcon);
        tvOpeningBalance = findViewById(R.id.tvOpeningBalance);
        tvClosingBalance = findViewById(R.id.tvClosingBalance);
        tvTotalIncome= findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvBalanceTrend = findViewById(R.id.tvBalanceTrend);
        rvRecentTransactions = findViewById(R.id.rvRecentTransactions);
        emptyRecentTransactions = findViewById(R.id.emptyRecentTransactions);
        tvViewAll = findViewById(R.id.tvViewAll);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        chipGroupDateRange = findViewById(R.id.chipGroupDateRange);
        chipToday = findViewById(R.id.chipToday);
        chipThisWeek = findViewById(R.id.chipThisWeek);
        chipThisMonth = findViewById(R.id.chipThisMonth);
        cardExpenseChart = findViewById(R.id.cardExpenseChart);
        pieChartExpenses = findViewById(R.id.pieChartExpenses);
        tvNoExpenseData = findViewById(R.id.tvNoExpenseData);
        recentTransactions = new ArrayList<>();
        fabSpeedDial = findViewById(R.id.fabSpeedDial);
        fabOverlay = findViewById(R.id.fabOverlay);
        fabMenuLayout = findViewById(R.id.fabMenuLayout);
        fabAddIncome = findViewById(R.id.fabAddIncome);
        fabAddExpense = findViewById(R.id.fabAddExpense);
        fabSearchTransactions = findViewById(R.id.fabSearchTransactions);

        cardBudgetProgress = findViewById(R.id.cardBudgetProgress);
        tvSetBudget = findViewById(R.id.tvSetBudget);
        tvBudgetSpent = findViewById(R.id.tvBudgetSpent);
        tvBudgetRemaining = findViewById(R.id.tvBudgetRemaining);
        tvBudgetPercentage = findViewById(R.id.tvBudgetPercentage);
        progressBudget = findViewById(R.id.progressBudget);
        // Charts
        lineChartBalance = findViewById(R.id.lineChartBalance);
        tvNoBalanceData = findViewById(R.id.tvNoBalanceData);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Expense Tracker");
            }
        }
    }

    private void setGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        int iconRes;

        if (hourOfDay >= 5 && hourOfDay < 12) {
            greeting = "Good Morning";
            iconRes = R.drawable.ic_sun;
        } else if (hourOfDay >= 12 && hourOfDay < 17) {
            greeting = "Good Afternoon";
            iconRes = R.drawable.ic_sun;
        } else if (hourOfDay >= 17 && hourOfDay < 21) {
            greeting = "Good Evening";
            iconRes = R.drawable.ic_moon;
        } else {
            greeting = "Good Night";
            iconRes = R.drawable.ic_moon;
        }

        tvGreeting.setText(greeting);
        ivGreetingIcon.setImageResource(iconRes);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(
                R.color.primary_color,
                R.color.accent_color,
                R.color.success_color
        );

        swipeRefresh.setOnRefreshListener(() -> {
            Log.d(TAG, "Refreshing data...");
            loadData();
        });
    }

    private void setupDateRangeChips() {
        chipToday.setOnClickListener(v -> {
            selectedRange = "today";
            updateDateRange("today");
            loadData();
        });

        chipThisWeek.setOnClickListener(v -> {
            selectedRange = "week";
            updateDateRange("week");
            loadData();
        });

        chipThisMonth.setOnClickListener(v -> {
            selectedRange = "month";
            updateDateRange("month");
            loadData();
        });
    }

    private void updateDateRange(String range) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        switch (range) {
            case "today":
                startDate = dateFormat.format(calendar.getTime());
                endDate = startDate;
                tvDate.setText("Today: " + displayFormat.format(calendar.getTime()));
                break;

            case "week":
                // Get start of week (Monday)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                startDate = dateFormat.format(calendar.getTime());

                // Get end of week (Sunday)
                calendar.add(Calendar.DAY_OF_WEEK, 6);
                endDate = dateFormat.format(calendar.getTime());

                tvDate.setText("This Week: " + displayFormat.format(calendar.getTime()));
                break;

            case "month":
                // Get start of month
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                startDate = dateFormat.format(calendar.getTime());

                // Get end of month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                endDate = dateFormat.format(calendar.getTime());

                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                tvDate.setText("This Month: " + monthFormat.format(calendar.getTime()));
                break;
        }

        // Set yesterday date for comparison
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        yesterdayDate = dateFormat.format(yesterday.getTime());

        Log.d(TAG, "Date range updated - Start: " + startDate + ", End: " + endDate);
    }


    private void setupRecentTransactions() {
        recentTransactionAdapter = new TransactionAdapter(recentTransactions, this);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvRecentTransactions.setAdapter(recentTransactionAdapter);
        rvRecentTransactions.setNestedScrollingEnabled(false);

        tvViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchTransactionActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        if (currentUser == null) return;

        FirebaseHelper.getInstance().getUser(currentUser.getUid(), new FirebaseHelper.OnUserListener() {
            @Override
            public void onSuccess(User user) {
                userProfile = user;
                if (user.getCompanyId() != null) {
                    userCompanyId = user.getCompanyId();
                }
                updateWelcomeMessage();
                loadData();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(MainActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                createUserProfileFromAuth();
            }
        });
    }

    private void createUserProfileFromAuth() {
        if (currentUser == null) return;

        String username = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
        String email = currentUser.getEmail();
        String role = email != null && (email.contains("admin@")) ? "ADMIN" : "USER";
        String companyId = getCompanyIdFromEmail(email);

        User user = new User(currentUser.getUid(), username, email, role,
                true, System.currentTimeMillis(), 0.0, companyId);

        FirebaseHelper.getInstance().createUser(user, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess(String message) {
                userProfile = user;
                userCompanyId = companyId;
                updateWelcomeMessage();
                loadData();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(MainActivity.this, "Error creating profile", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getCompanyIdFromEmail(String email) {
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf("@") + 1);
            return domain.replace(".", "_").toLowerCase();
        }
        return "default_company";
    }

    private void updateWelcomeMessage() {
        if (tvWelcome != null && userProfile != null) {
            tvWelcome.setText("Welcome, " + userProfile.getUsername());
        }
    }

    private void loadData() {
        if (userCompanyId == null) return;

        loadRangeSummary();
        loadRecentTransactions();
        loadExpenseBreakdown();
        updateBudgetProgress();
        load7DayBalanceTrend();
    }

    private void loadRangeSummary() {
        Log.d(TAG, "Loading summary for range: " + startDate + " to " + endDate);

        // For today, use existing summary method
        if ("today".equals(selectedRange)) {
            BalanceCalculator.getDailySummary(userCompanyId, startDate,
                    new BalanceCalculator.OnDailySummaryListener() {
                        @Override
                        public void onSummaryCalculated(BalanceCalculator.DailySummary summary) {
                            updateSummaryUI(summary);
                            calculateTrends(summary);
                            swipeRefresh.setRefreshing(false);
                        }

                        @Override
                        public void onSummaryError(String error) {
                            swipeRefresh.setRefreshing(false);
                        }
                    });
        } else {
            // For week/month, calculate range summary
            loadRangeTransactionsForSummary();
        }
    }

    private void loadRangeTransactionsForSummary() {
        Log.d(TAG, "Loading range transactions from " + startDate + " to " + endDate);

        FirebaseHelper.getInstance().getAllTransactionsForCompany(userCompanyId,
                new FirebaseHelper.OnTransactionListListener() {
                    @Override
                    public void onSuccess(List<Transaction> allTransactions) {
                        Log.d(TAG, "Retrieved " + allTransactions.size() + " total transactions");

                        // Filter transactions within date range
                        List<Transaction> rangeTransactions = new ArrayList<>();
                        for (Transaction t : allTransactions) {
                            if (t.getDate().compareTo(startDate) >= 0 &&
                                    t.getDate().compareTo(endDate) <= 0) {
                                rangeTransactions.add(t);
                            }
                        }

                        Log.d(TAG, "Filtered to " + rangeTransactions.size() + " transactions in range");

                        // Calculate summary
                        BalanceCalculator.DailySummary summary = new BalanceCalculator.DailySummary();

                        if (!rangeTransactions.isEmpty()) {
                            // Sort by date and timestamp
                            rangeTransactions.sort((t1, t2) -> {
                                int dateCompare = t1.getDate().compareTo(t2.getDate());
                                if (dateCompare == 0) {
                                    return Long.compare(t1.getTimestamp(), t2.getTimestamp());
                                }
                                return dateCompare;
                            });

                            summary.openingBalance = rangeTransactions.get(0).getOpeningBalance();
                            summary.closingBalance = rangeTransactions.get(rangeTransactions.size() - 1).getClosingBalance();

                            for (Transaction t : rangeTransactions) {
                                if ("INCOME".equals(t.getType())) {
                                    summary.totalIncome += t.getAmount();
                                } else if ("EXPENSE".equals(t.getType())) {
                                    summary.totalExpense += t.getAmount();
                                }
                            }
                        }

                        Log.d(TAG, "Summary calculated - Income: " + summary.totalIncome +
                                ", Expense: " + summary.totalExpense);

                        updateSummaryUI(summary);
                        swipeRefresh.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to load range transactions: " + error);
                        swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void calculateTrends(BalanceCalculator.DailySummary todaySummary) {
        BalanceCalculator.getDailySummary(userCompanyId, yesterdayDate,
                new BalanceCalculator.OnDailySummaryListener() {
                    @Override
                    public void onSummaryCalculated(BalanceCalculator.DailySummary yesterdaySummary) {
                        double balanceChange = todaySummary.closingBalance - yesterdaySummary.closingBalance;
                        double balancePercentage = yesterdaySummary.closingBalance != 0
                                ? (balanceChange / Math.abs(yesterdaySummary.closingBalance)) * 100
                                : 0;

                        if (tvBalanceTrend != null && balancePercentage != 0) {
                            String trendText = balanceChange > 0
                                    ? String.format(Locale.getDefault(), "↑ %.1f%%", Math.abs(balancePercentage))
                                    : String.format(Locale.getDefault(), "↓ %.1f%%", Math.abs(balancePercentage));
                            tvBalanceTrend.setText(trendText);
                            tvBalanceTrend.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onSummaryError(String error) {
                        // Ignore error
                    }
                });
    }

    private void updateSummaryUI(BalanceCalculator.DailySummary summary) {
        if (summary == null) {
            summary = new BalanceCalculator.DailySummary();
        }

        tvOpeningBalance.setText(String.format(Locale.getDefault(), "₹%.2f", summary.openingBalance));
        tvClosingBalance.setText(String.format(Locale.getDefault(), "₹%.2f", summary.closingBalance));
        tvTotalIncome.setText(String.format(Locale.getDefault(), "₹%.2f", summary.totalIncome));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "₹%.2f", summary.totalExpense));
    }

    private void loadRecentTransactions() {
        if (userCompanyId == null) return;

        Log.d(TAG, "Loading recent transactions for range: " + startDate + " to " + endDate);

        if ("today".equals(selectedRange)) {
            // For today, load transactions for that specific date
            FirebaseHelper.getInstance().getTransactionsByDate(userCompanyId, startDate,
                    new FirebaseHelper.OnTransactionListListener() {
                        @Override
                        public void onSuccess(List<Transaction> transactions) {
                            updateRecentTransactionsList(transactions);
                        }

                        @Override
                        public void onFailure(String error) {
                            showEmptyTransactions();
                        }
                    });
        } else {
            // For week/month, get all transactions and filter by range
            FirebaseHelper.getInstance().getAllTransactionsForCompany(userCompanyId,
                    new FirebaseHelper.OnTransactionListListener() {
                        @Override
                        public void onSuccess(List<Transaction> allTransactions) {
                            List<Transaction> rangeTransactions = new ArrayList<>();
                            for (Transaction t : allTransactions) {
                                if (t.getDate().compareTo(startDate) >= 0 &&
                                        t.getDate().compareTo(endDate) <= 0) {
                                    rangeTransactions.add(t);
                                }
                            }

                            // Sort by date and timestamp descending
                            rangeTransactions.sort((t1, t2) -> {
                                int dateCompare = t2.getDate().compareTo(t1.getDate());
                                if (dateCompare == 0) {
                                    return Long.compare(t2.getTimestamp(), t1.getTimestamp());
                                }
                                return dateCompare;
                            });

                            updateRecentTransactionsList(rangeTransactions);
                        }

                        @Override
                        public void onFailure(String error) {
                            showEmptyTransactions();
                        }
                    });
        }
    }

    private void updateRecentTransactionsList(List<Transaction> transactions) {
        recentTransactions.clear();

        // Take only last 5 transactions
        int count = Math.min(5, transactions.size());
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                recentTransactions.add(transactions.get(i));
            }
        }

        recentTransactionAdapter.notifyDataSetChanged();

        if (recentTransactions.isEmpty()) {
            showEmptyTransactions();
        } else {
            rvRecentTransactions.setVisibility(View.VISIBLE);
            emptyRecentTransactions.setVisibility(View.GONE);
        }
    }

    private void showEmptyTransactions() {
        rvRecentTransactions.setVisibility(View.GONE);
        emptyRecentTransactions.setVisibility(View.VISIBLE);
    }

    private void loadExpenseBreakdown() {
        Log.d(TAG, "Loading expense breakdown for: " + startDate + " to " + endDate);

        FirebaseHelper.getInstance().getAllTransactionsForCompany(userCompanyId,
                new FirebaseHelper.OnTransactionListListener() {
                    @Override
                    public void onSuccess(List<Transaction> allTransactions) {
                        // Filter expenses within date range
                        Map<String, Float> categoryExpenses = new HashMap<>();

                        for (Transaction t : allTransactions) {
                            if ("EXPENSE".equals(t.getType()) &&
                                    t.getDate().compareTo(startDate) >= 0 &&
                                    t.getDate().compareTo(endDate) <= 0) {

                                String category = t.getCategory();
                                if (category != null && !category.trim().isEmpty()) {
                                    float amount = (float) t.getAmount();
                                    categoryExpenses.put(category,
                                            categoryExpenses.getOrDefault(category, 0f) + amount);
                                }
                            }
                        }

                        Log.d(TAG, "Found " + categoryExpenses.size() + " expense categories");

                        if (categoryExpenses.isEmpty()) {
                            cardExpenseChart.setVisibility(View.GONE);
                        } else {
                            cardExpenseChart.setVisibility(View.VISIBLE);
                            setupPieChart(categoryExpenses);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to load expense breakdown: " + error);
                        cardExpenseChart.setVisibility(View.GONE);
                    }
                });
    }

    private void setupPieChart(Map<String, Float> categoryExpenses) {
        // Clear previous data
        pieChartExpenses.clear();

        List<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Float> entry : categoryExpenses.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            tvNoExpenseData.setVisibility(View.VISIBLE);
            pieChartExpenses.setVisibility(View.GONE);
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Use consistent colors
        int[] colors = new int[]{
                Color.parseColor("#FF6384"),
                Color.parseColor("#36A2EB"),
                Color.parseColor("#FFCE56"),
                Color.parseColor("#4BC0C0"),
                Color.parseColor("#9966FF"),
                Color.parseColor("#FF9F40"),
                Color.parseColor("#FF6384"),
                Color.parseColor("#C9CBCF")
        };
        dataSet.setColors(colors);

        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartExpenses));

        pieChartExpenses.setData(data);
        pieChartExpenses.setUsePercentValues(true);
        pieChartExpenses.getDescription().setEnabled(false);
        pieChartExpenses.setDrawHoleEnabled(true);
        pieChartExpenses.setHoleColor(Color.TRANSPARENT);
        pieChartExpenses.setHoleRadius(45f);
        pieChartExpenses.setTransparentCircleRadius(50f);
        pieChartExpenses.setDrawEntryLabels(false);
        pieChartExpenses.setRotationEnabled(true);
        pieChartExpenses.setHighlightPerTapEnabled(true);

        pieChartExpenses.getLegend().setEnabled(true);
        pieChartExpenses.getLegend().setTextSize(11f);
        pieChartExpenses.getLegend().setFormSize(10f);
        pieChartExpenses.getLegend().setWordWrapEnabled(true);

        pieChartExpenses.animateY(1000);
        pieChartExpenses.invalidate();

        tvNoExpenseData.setVisibility(View.GONE);
        pieChartExpenses.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        Toast.makeText(this, "Transaction: " + transaction.getCategory(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditClick(Transaction transaction) {
        Intent intent = new Intent(this, AddTransactionActivity.class);
        intent.putExtra("transaction_id", transaction.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Transaction transaction) {
        Toast.makeText(this, "Use search page to delete transactions", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(item.getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0);
            item.setTitle(spanString);
        }

        MenuItem manageUsersItem = menu.findItem(R.id.action_manage_users);
        if (manageUsersItem != null && userProfile != null) {
            manageUsersItem.setVisible("ADMIN".equals(userProfile.getRole()));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_transaction) {
            startActivity(new Intent(this, AddTransactionActivity.class));
            return true;
        } else if (id == R.id.action_search_transactions) {
            startActivity(new Intent(this, SearchTransactionActivity.class));
            return true;
        } else if (id == R.id.action_manage_users) {
            startActivity(new Intent(this, ManageUsersActivity.class));
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_theme) {
            showThemeDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showThemeDialog() {
        String[] themeOptions = {"Light", "Dark", "System Default"};
        int currentTheme = ThemeManager.getSavedTheme(this);

        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themeOptions, currentTheme, (dialog, which) -> {
                    ThemeManager.saveTheme(this, which);
                    dialog.dismiss();
                    Toast.makeText(this, "Theme changed to: " + ThemeManager.getThemeName(which),
                            Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupSpeedDial() {
        fabSpeedDial.setOnClickListener(v -> toggleSpeedDialMenu());

        fabOverlay.setOnClickListener(v -> closeSpeedDialMenu());

        fabAddIncome.setOnClickListener(v -> {
            closeSpeedDialMenu();
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("default_type", "INCOME");
            startActivity(intent);
        });

        fabAddExpense.setOnClickListener(v -> {
            closeSpeedDialMenu();
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("default_type", "EXPENSE");
            startActivity(intent);
        });

        fabSearchTransactions.setOnClickListener(v -> {
            closeSpeedDialMenu();
            startActivity(new Intent(this, SearchTransactionActivity.class));
        });

        tvSetBudget.setOnClickListener(v -> showSetBudgetDialog());
    }

    private void toggleSpeedDialMenu() {
        if (isFabMenuOpen) {
            closeSpeedDialMenu();
        } else {
            openSpeedDialMenu();
        }
    }

    private void openSpeedDialMenu() {
        isFabMenuOpen = true;
        fabMenuLayout.setVisibility(View.VISIBLE);
        fabOverlay.setVisibility(View.VISIBLE);

        // Rotate FAB icon
        fabSpeedDial.animate().rotation(45f).setDuration(300).start();

        // Animate menu items
        fabMenuLayout.setAlpha(0f);
        fabMenuLayout.setTranslationY(100f);
        fabMenuLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
    }

    private void closeSpeedDialMenu() {
        isFabMenuOpen = false;

        // Rotate FAB icon back
        fabSpeedDial.animate().rotation(0f).setDuration(300).start();

        // Animate menu items out
        fabMenuLayout.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .withEndAction(() -> {
                    fabMenuLayout.setVisibility(View.GONE);
                    fabOverlay.setVisibility(View.GONE);
                })
                .start();
    }

    private void showSetBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_budget, null);

        EditText etBudget = dialogView.findViewById(R.id.etBudgetAmount);

        // Show current budget if set
        if (monthlyBudget > 0) {
            etBudget.setText(String.valueOf((int) monthlyBudget));
        }

        builder.setView(dialogView)
                .setTitle("Set Monthly Budget")
                .setPositiveButton("Save", (dialog, which) -> {
                    String budgetStr = etBudget.getText().toString().trim();
                    if (!budgetStr.isEmpty()) {
                        try {
                            monthlyBudget = Double.parseDouble(budgetStr);
                            saveBudgetToPreferences(monthlyBudget);
                            updateBudgetProgress();
                            Toast.makeText(this, "Budget set successfully", Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid budget amount", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveBudgetToPreferences(double budget) {
        getSharedPreferences("ExpenseTracker", MODE_PRIVATE)
                .edit()
                .putFloat("monthly_budget", (float) budget)
                .apply();
    }

    private double loadBudgetFromPreferences() {
        return getSharedPreferences("ExpenseTracker", MODE_PRIVATE)
                .getFloat("monthly_budget", 0f);
    }

    private void updateBudgetProgress() {
        monthlyBudget = loadBudgetFromPreferences();

        if (monthlyBudget > 0 && "month".equals(selectedRange)) {
            cardBudgetProgress.setVisibility(View.VISIBLE);

            // Get current month's expenses from summary
            BalanceCalculator.getDailySummary(userCompanyId, startDate,
                    new BalanceCalculator.OnDailySummaryListener() {
                        @Override
                        public void onSummaryCalculated(BalanceCalculator.DailySummary summary) {
                            double spent = summary.totalExpense;
                            double remaining = monthlyBudget - spent;
                            double percentage = (spent / monthlyBudget) * 100;

                            // Animate values
                            animateValue(tvBudgetSpent, 0, spent, "₹%.0f spent");
                            animateValue(tvBudgetRemaining, 0, remaining, "₹%.0f left");

                            // Update progress bar
                            progressBudget.setMax((int) monthlyBudget);
                            progressBudget.setProgress((int) spent);

                            // Update percentage text
                            tvBudgetPercentage.setText(String.format(Locale.getDefault(),
                                    "%.1f%% of budget used", percentage));

                            // Change color based on usage
                            if (percentage >= 90) {
                                progressBudget.setProgressDrawable(
                                        getResources().getDrawable(R.drawable.progress_budget_danger));
                                tvBudgetRemaining.setTextColor(getResources().getColor(R.color.error_color));
                            } else if (percentage >= 75) {
                                progressBudget.setProgressDrawable(
                                        getResources().getDrawable(R.drawable.progress_budget_warning));
                                tvBudgetRemaining.setTextColor(getResources().getColor(R.color.warning_color));
                            } else {
                                progressBudget.setProgressDrawable(
                                        getResources().getDrawable(R.drawable.progress_budget));
                                tvBudgetRemaining.setTextColor(getResources().getColor(R.color.success_color));
                            }
                        }

                        @Override
                        public void onSummaryError(String error) {
                            // Keep card visible but show zero values
                        }
                    });
        } else {
            cardBudgetProgress.setVisibility(View.GONE);
        }
    }

    private void animateValue(TextView textView, double start, double end, String format) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat((float) start, (float) end);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            textView.setText(String.format(Locale.getDefault(), format, value));
        });
        animator.start();
    }
    private void load7DayBalanceTrend() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        List<String> dates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) calendar.clone();
            day.add(Calendar.DAY_OF_MONTH, -i);
            dates.add(dateFormat.format(day.getTime()));
        }

        List<Float> balances = new ArrayList<>();
        final int[] processedDays = {0};

        for (String date : dates) {
            BalanceCalculator.getDailySummary(userCompanyId, date,
                    new BalanceCalculator.OnDailySummaryListener() {
                        @Override
                        public void onSummaryCalculated(BalanceCalculator.DailySummary summary) {
                            balances.add((float) summary.closingBalance);
                            processedDays[0]++;

                            if (processedDays[0] == dates.size()) {
                                setupLineChart(balances, dates);
                            }
                        }

                        @Override
                        public void onSummaryError(String error) {
                            balances.add(0f);
                            processedDays[0]++;

                            if (processedDays[0] == dates.size()) {
                                setupLineChart(balances, dates);
                            }
                        }
                    });
        }
    }

    private void setupLineChart(List<Float> balances, List<String> dates) {
        if (balances.isEmpty() || balances.stream().allMatch(b -> b == 0f)) {
            lineChartBalance.setVisibility(View.GONE);
            tvNoBalanceData.setVisibility(View.VISIBLE);
            return;
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < balances.size(); i++) {
            entries.add(new Entry(i, balances.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Balance");
        dataSet.setColor(getResources().getColor(R.color.primary_color));
        dataSet.setCircleColor(getResources().getColor(R.color.primary_color));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.primary_color));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        lineChartBalance.setData(lineData);

        // Customize chart
        lineChartBalance.getDescription().setEnabled(false);
        lineChartBalance.getLegend().setEnabled(false);
        lineChartBalance.setDrawGridBackground(false);
        lineChartBalance.getAxisRight().setEnabled(false);
        lineChartBalance.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        lineChartBalance.getXAxis().setDrawGridLines(false);
        lineChartBalance.getAxisLeft().setDrawGridLines(true);
        lineChartBalance.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        lineChartBalance.setTouchEnabled(true);
        lineChartBalance.setDragEnabled(true);
        lineChartBalance.setScaleEnabled(false);
        lineChartBalance.setPinchZoom(false);

        // Set X-axis labels (show day names)
        lineChartBalance.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dates.size()) {
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", Locale.getDefault());
                        Date date = inputFormat.parse(dates.get(index));
                        return outputFormat.format(date);
                    } catch (Exception e) {
                        return "";
                    }
                }
                return "";
            }
        });

        lineChartBalance.animateX(1000);
        lineChartBalance.invalidate();

        lineChartBalance.setVisibility(View.VISIBLE);
        tvNoBalanceData.setVisibility(View.GONE);
    }
}